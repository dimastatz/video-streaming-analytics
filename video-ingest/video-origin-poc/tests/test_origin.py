import time
import unittest
from cdn_origin.origin import Origin


class TestOrigin(unittest.TestCase):
    def upload_dummy(self, dest_name: str, data: bytes, job_types: str, info: None):
        print(dest_name, data, job_types, info)
        time.sleep(1)


    def test_upper(self):
        x = Origin(self.upload_dummy)
        x.upload('dummy1', b'some_data', 'type_a', 'info')
        x.upload('dummy2', b'some_data', 'type_a', 'info')
        
        finished, running = x.wait(0.5)
        self.assertEqual(len(running), 2)
        
        finished, running = x.wait(2)
        self.assertEqual(len(finished), 2)
        



