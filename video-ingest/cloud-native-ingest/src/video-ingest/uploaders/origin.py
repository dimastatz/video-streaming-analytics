from queue import Queue
from typing import Callable
from concurrent.futures import ThreadPoolExecutor, Future


class Origin:
    def __init__(self, upload_handler, pool_size=16):
        self.results = Queue()
        self.pool = ThreadPoolExecutor(pool_size)
        self.upload_handler: Callable = upload_handler
        

    def upload(self, job_type, outFilename, data, info=None):
        self.results.put(self.pool.submit(self.upload_handler, job_type, outFilename, data))
        

    def wait(self, timeout: int):
        while not self.results.
            future: Future = self.results.get_nowait()
            
        
    