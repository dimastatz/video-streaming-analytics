"""
Manages uploading files from encoders to origins.
Also deals with multiple origins, timeouts, retries, and parallelism of uploads
"""
from __future__ import absolute_import
from __future__ import print_function
import os
import threading
import time
from six.moves import queue as Queue
from uplynkcore.logger import exlog, ilog
from uplynkcore.metric.metric_batch import MetricBatcher
import uplynkrepo.constants as constants
from uplynkrepo.storage import Storage
from uplynkrepo.py3_helpers import py3_bytes
from version import version
try:
    zone = open('/opt/uplynk/SERVER_ZONE', 'rb').read().decode('utf-8')
except Exception:
    zone = 'unknown'
METRIC_BATCHER = MetricBatcher(60)
METRIC_BATCHER.add_dimension('version', version)
METRIC_BATCHER.add_dimension('zone', zone)
class GV:
    """
    Place for global vars
    """
    encoderID = "na"
if os.path.exists('/opt/uplynk/SERVER_ID'):
    GV.encoderID = open('/opt/uplynk/SERVER_ID').read().strip()
MAX_UPLOAD_RETRIES = 10
UPLOAD_THREADS_PER_ORIGIN = 64
class OriginSet(object):
    """
    Takes a work token as input.  If the work token has multiple upload destinations
    (work.dest_info and work.alt_dest_infos), then it will manage uploads to all of them.
    UploadFile is called for each file to be uploaded for this work token.
    WaitForUploads is called once all files for the work token have been given to UploadFile.
      Will return when:
       1) All files have been successfully uploaded to all destinations
       2) All files have either been successfully uploaded, or have errored out for all destinations
       3) All files have either been successfully uploaded, or have errored out for at least one
         destination, and
         secondary_timeout seconds have elapsed since WaitForUploads was called
      Returns a list of destination IDs to which all files were successfully uploaded
    """
    def __init__(self, work, LogS3Error, lib, info=None):
        self.dests = {}
        self.lib = lib
        self.info = info
        # first, make sure the primary origin is ready to go
        if work.dest_info:
            self.add_origin(work.dest_info, LogS3Error)
        # next, make sure all additional origins are ready
        for dest in work.get('alt_dest_infos', []):
            self.add_origin(dest, LogS3Error)
        if len(self.dests) <= 0:
            raise Exception("No destinations available")
        # finally, tell all destinations to notify us when they're done
        self.notifier = Notifier()
        for _, dest in self.dests.items():
            dest.set_notifier(self.notifier)
    def add_origin(self, dest_info, LogS3Error):
        """
        Adds an origin to the origin set to upload to
        """
        key = storage_key(dest_info)
        if key not in self.dests:
            self.dests[key] = Origin(dest_info, LogS3Error, self.lib)
        else:
            ilog("[OriginSet] Skipping previously added origin: %s", key)
        ilog("[OriginSet] Using origins %s", list(self.dests.keys()))
    def UploadFile(self, job_type, outFilename, data):
        """
        Adds a file to every origin's upload queue
        """
        ilog("[OriginSet] Adding File %s", outFilename)
        for _, origin in self.dests.items():
            origin.upload(job_type, outFilename, data, self.info)
    def WaitForUploads(self, secondary_timeout):
        """
        Waits for all origins to complete, or any origin to be completed by secondary_timeout
        """
        ilog("[OriginSet] Waiting for uploads")
        # tell origins that there are no more files coming
        for _, origin in self.dests.items():
            origin.finish()
        # wait for at least one to finish
        start = time.time()
        self.notifier.wait()
        # give the others a chance to finish
        while self.notifier.complete < len(self.dests) and time.time() - start < secondary_timeout:
            elapsed = time.time() - start
            self.notifier.wait(max(0.001, secondary_timeout - elapsed))
        # tell origins that haven't finished to give up
        for k, origin in self.dests.items():
            if not origin.finished_ok:
                origin.give_up()
            origin.cleanup()
        completed_origins = tuple(origin.dest_info['storage_id'] \
                            for origin in self.dests.values() if origin.finished_ok)
        ilog("[OriginSet] Done %s", completed_origins)
        # tell the caller which origins completed on time
        return completed_origins
    def GetInitsSeen(self):
        inits_seen = 0
        for _, origin in self.dests.items():
            inits_seen += origin.get_inits_seen()
        return inits_seen
class Origin(object):  # TODO: auto cleanup after so much time of inactivity
    """
    Negotiates uploading N files to 1 origin.
    Keeps track of success/retry/failure
    Notifies a notifier when completed
    Can be told to give up
    """
    def __init__(self, dest_info, LogS3Error, lib):
        self.LogS3Error = LogS3Error
        self.lib = lib
        self.dest_info = dest_info
        self.keep_running = True
        self.input_queue = Queue.Queue()
        self.lock = threading.Lock()
        self.set_notifier(None)  # initialize notifier and other flags
        self.give_up_trying = False
        self.finished = False
        self.all_uploaded_ok = True
        self.finished_ok = False
        self.init_files_seen = 0
        # start threads
        self.threads = []
        for _ in range(UPLOAD_THREADS_PER_ORIGIN):
            self.threads.append(start_thread(self.upload_worker))
        ilog("[Origin-%s] Initialized", self.dest_info['storage_id'])
    def check_threads(self):
        """
        Periodically check that our threads are alive and restart them if they die
        """
        for thread in self.threads[:]:
            if not thread.isAlive():
                self.threads.remove(thread)
                ilog('[Origin-%s] Restarting upload thread', self.dest_info['storage_id'])
                self.threads.append(start_thread(self.upload_worker))
    def upload(self, job_type, outFilename, data, info=None):
        """
        Adds a file to our upload queue
        """
        self.check_threads()
        # TODO: not sure if we really need to lock here,
        # since this /should/ always be called on the same thread as self.finish
        self.lock.acquire()
        self.input_queue.put((job_type, outFilename, data, info))
        self.file_count += 1
        self.lock.release()
    def set_notifier(self, notifier):
        """
        Sets the notifier to tell when we are complete
        """
        self.notifier = notifier
        self.file_count = 0
        self.finished_ok = False
        self.finished = False
        self.all_uploaded_ok = True
        self.give_up_trying = False
    def finish(self):
        """
        Sets our final state (ok / all uploaded ok)
        Then notifies our notifier
        """
        self.lock.acquire()
        self.finished = True
        if self.file_count == 0:
            self.finished_ok = self.all_uploaded_ok
            self.notifier.notify()
        self.lock.release()
    def give_up(self):
        '''
        Clean out upload queues and signal upload workers to exit
        '''
        ilog('[Origin-%s] Giving up', self.dest_info['storage_id'])
        self.give_up_trying = True
        while self.input_queue.qsize() > 0:
            try:
                self.input_queue.get_nowait()
            except Queue.Empty():
                pass
    def get_inits_seen(self):
        return self.init_files_seen
    def cleanup(self):
        '''
        Signal workers to shutdown
        Join all threads
        '''
        ilog('[Origin-%s] Cleaning up', self.dest_info['storage_id'])
        # tell worker threads to shut down
        self.keep_running = False
        self.give_up_trying = True
        for _ in range(len(self.threads)):
            self.input_queue.put(None)
        # wait for threads to shut down
        for thread in self.threads:
            thread.join()
        self.threads = []
        ilog('[Origin-%s] Cleaned', self.dest_info['storage_id'])
    def upload_worker(self):
        '''
        Read from upload queues and send to uploader
        '''
        uploader = None
        try:
            uploader = Storage.FromStorageInfo('push', self.dest_info, useCS3=True, lib=self.lib)
            uploader.SetPublicRead(True)
            if callable(getattr(uploader, 'SetUserAgent', None)):
                uploader.SetUserAgent("uplynk encoder 1.0")
        except Exception:
            exlog('[Origin-%s-W] Error initializing storage', self.dest_info['storage_id'])
            
        try:
            while self.keep_running:
                # wait for upload work
                msg = self.input_queue.get()
                if msg is None:
                    break
                job_type, outFilename, data, info = msg
                try:
                    cloud_type_name = ("" if getattr(uploader, 'cloud', None) is None else "/" + uploader.cloud)
                    ilog('[Origin-%s%s-W] File %s: Starting',
                         self.dest_info['storage_id'], cloud_type_name, outFilename)
                    #perform upload
                    if not self.perform_upload(uploader, job_type, outFilename, data, info):
                        ilog('[Origin-%s%s-W] File %s: Failed upload',
                             self.dest_info['storage_id'], cloud_type_name, outFilename)
                        self.all_uploaded_ok = False
                    else:
                        ilog('[Origin-%s%s-W] File %s: Uploaded',
                             self.dest_info['storage_id'], cloud_type_name, outFilename)
                    # notify about completion
                    self.lock.acquire()
                    self.file_count -= 1
                    if self.file_count == 0 and self.finished:
                        self.finished_ok = self.all_uploaded_ok
                        self.notifier.notify()
                    self.lock.release()
                except Exception as e:
                    exlog('[Origin-%s-W] File %s: Error: %s',
                          self.dest_info['storage_id'],
                          outFilename,
                          str(e))
        except Exception:
            exlog('[Origin-%s-W] Error performing upload', self.dest_info['storage_id'])
        finally:
            # clean up uploader handle
            if uploader:
                # If the uploader did not initialize properly, it won't be available, and this will
                # throw an exception.
                uploader.Close()
            ilog('[Origin-%s-W] Upload worker exiting', self.dest_info['storage_id'])
    def perform_upload(self, uploader, job_type, outFilename, data, info=None):
        '''
        Upload a single file, try to do it well (use timeouts and retries)
        '''
        if not uploader:
            ilog('[Origin-%s-W] No uploader', self.dest_info['storage_id'])
            return False
        if "_init." in outFilename:
            self.init_files_seen += 1
        total_retries = 0
        try:
            if os.getenv('UPLYNK_IGNOREUPLOADTIMEOUTS', None) is None:
                cbP2C = getattr(info, 'callback', None)
                if job_type == 'live' and cbP2C is None:
                    if len(data) < 1048576: # if < 1MB
                        timeout = 1000
                    else:
                        timeout = 2000
                else:
                    timeout = 30000
                if hasattr(uploader, 'SetBandwidthTimeout'):
                    # time out if the upload bandwidth is lower than minKbps for longer than maxMS
                    uploader.SetBandwidthTimeout(minKbps=700, maxMS=4000)
            else:
                timeout = 30000
            for retry in range(MAX_UPLOAD_RETRIES):
                if self.give_up_trying:
                    break
                try:
                    beamID = self.dest_info['beamID']
                    ilog('[Origin-%s-U] File %s: %s upload with timeout %s and size %d and beam %s',
                         self.dest_info['storage_id'], outFilename,
                         "Starting" if retry == 0 else "Retrying", timeout, len(data), beamID)
                    contentType = 'application/octet-stream'
                    if outFilename.endswith('.jpg'):
                        contentType = 'image/jpeg'
                    elif outFilename.endswith('.m4s'): # segment type used for subtitles in fmp4
                        contentType = 'application/mp4'
                    elif outFilename.endswith('.vtt'):
                        contentType = 'text/vtt'
                    
                    uploader.Upload(outFilename,
                                    py3_bytes(data),
                                    timeout=timeout,
                                    dnsTimeout=-1 if retry == 0 else 0,
                                    contentType=contentType, 
                                    info=info)
                    ilog('[Origin-%s-U] File %s: Finished upload, Beam: %s',
                         self.dest_info['storage_id'], outFilename, beamID)
                    size_dim = "size:%s" % ("LT_1_MB" if len(data) < 1048576 else "GT_1_MB")
                    METRIC_BATCHER.inc('enc_ul_upload_retries',
                                       dimensions=["retries:%s" % retry,
                                                   size_dim,
                                                   "uploadStatus:%s" % "SUCCESS"])
                    return True
                except Exception as e:
                    if uploader.cloud == constants.AZURE:
                        ilog('exception: {}'.format(str(e)))
                        continue
                    METRIC_BATCHER.inc('enc_ul_timeout')
                    exlog.local(str(e))
                    self.LogS3Error('upload', outFilename, uploader)
                    ilog('[Origin-%s-U] File %s: headers in: %s',
                         self.dest_info['storage_id'], outFilename, uploader.headersIn)
                    ilog('[Origin-%s-U] File %s: headers out: %s',
                         self.dest_info['storage_id'], outFilename, uploader.headersOut)
                    if retry > 1:
                        # oh dear, we can't seem to upload faster than
                        # our min bandwidth after several tries
                        # disable the bandwidth limit for the next retries
                        ilog('[Origin-%s-U] File %s: Removing bandiwdth restriction and ' \
                             'doubling timeout',
                             self.dest_info['storage_id'], outFilename)
                        if hasattr(uploader, 'SetBandwidthTimeout'):
                            uploader.SetBandwidthTimeout(0, 0)
                    # retry with 2s, 4s a couple of times before going bigger
                    timeoutVals = [2, 2, 4, 4, 4, 7, 10, 15, 20, 25]
                    timeout = timeoutVals[retry] * 1000
                    total_retries = retry
        except Exception as e:
            exlog(str(e))
        dim_size = "size:%s" % ("LT_1_MB" if len(data) < 1048576 else "GT_1_MB")
        METRIC_BATCHER.inc('enc_ul_upload_retries', dimensions=["retries:%s" % total_retries,
                                                                dim_size,
                                                                "uploadStatus:%s" % "FAIL"])
        return False
def start_thread(target, *args, **kwargs):
    '''Helper method to start a thread'''
    thread = threading.Thread(target=target, args=args, kwargs=kwargs)
    thread.setDaemon(True)
    thread.start()
    return thread
class Notifier(object):
    '''Cheesy class to make it so we can wait on all origins simultaneously'''
    def __init__(self):
        # TODO: I worry about creating a Queue (and the resources like mutexes that go with it)
        # for every work token.  But the only alternative I can think of is to poll,
        # which seems like a bad idea...
        self.queue = Queue.Queue()
        self.complete = 0
    def notify(self):
        '''Origin class instances call this when they finish uploading'''
        self.queue.put(None)
    def wait(self, timeout=None):
        '''OriginSet calls this to wait for the next origin to finish'''
        try:
            self.queue.get(timeout=timeout)
        except Queue.Empty:
            pass
        else:
            self.complete += 1
def storage_key(dest_info):
    """
    not sure if storage_id is the right way to go,
    so made this a function to make it easy to change in the future
    """
    return dest_info['beamID'] + '_' + dest_info['storage_id']
