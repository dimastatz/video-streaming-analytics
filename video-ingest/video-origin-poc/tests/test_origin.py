import time
import unittest
from cdn_origin.origin import Origin


class TestOrigin(unittest.TestCase):
    def upload_dummy(self, dest_name: str, data: bytes, job_types: str, info: None):
        print(dest_name, data, job_types, info)
        time.sleep(0.1)

    def test_tasks_success(self):
        x = Origin(self.upload_dummy)
        x.upload('dummy1', b'some_data', 'type_a', 'info')
        x.upload('dummy2', b'some_data', 'type_a', 'info')
    
        finished, running = x.wait(0.01)
        self.assertEqual(len(running), 2)
        finished, running = x.wait(2)
        self.assertEqual(len(finished), 2)
        x.close()
        
    def test_callback(self):
        callback_finished = False

        def all_done_callback():
           nonlocal callback_finished
           callback_finished = True

        x = Origin(self.upload_dummy)
        x.add_all_done_callback(all_done_callback)

        x.upload('dummy1', b'some_data', 'type_a', 'info')
        time.sleep(0.05)
        assert(callback_finished == False)
        
        x.upload('dummy2', b'some_data', 'type_a', 'info')
        time.sleep(0.1)
        assert(callback_finished == False)
        
        time.sleep(0.1)
        assert(callback_finished == True)
        x.close()


