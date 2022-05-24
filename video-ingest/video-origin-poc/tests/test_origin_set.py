import time
import unittest
from cdn_origin.origin import Origin
from cdn_origin.origin_set import OriginSet


class TestOriginSet(unittest.TestCase):
    def upload_dummy_1(self, dest_name: str, data: bytes, job_types: str, info: None):
        print('upload_dummy_1', dest_name, data, job_types, info)
        time.sleep(0.1)

    def upload_dummy_2(self, dest_name: str, data: bytes, job_types: str, info: None):
        print('upload_dummy_2', dest_name, data, job_types, info)
        time.sleep(0.1)

    def upload_dummy_3(self, dest_name: str, data: bytes, job_types: str, info: None):
        print('upload_dummy_2', dest_name, data, job_types, info)
        time.sleep(0.5)

    def test_tasks_success(self):
        x = OriginSet([Origin(self.upload_dummy_1), Origin(self.upload_dummy_2)])
        x.upload('dummy', b'some_data', 'type_a', 'info')
        assert(x.wait(0.05) == False)
        assert(x.wait(0.09) == True)
        assert(x.wait(0.01) == True)
        x.close()


    def test_one_completed(self):
        x = OriginSet([Origin(self.upload_dummy_1), Origin(self.upload_dummy_3)])
        x.upload('dummy', b'some_data', 'type_a', 'info')
        assert(x.wait(0.05) == False)
        assert(x.wait(0.1) == True)
        x.close()
    

