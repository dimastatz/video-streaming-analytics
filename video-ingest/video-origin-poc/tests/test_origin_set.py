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

    def test_tasks_success(self):
        x = OriginSet([Origin(self.upload_dummy_1), Origin(self.upload_dummy_2)])
        x.upload('dummy', b'some_data', 'type_a', 'info')
        time.sleep(0.2)
        x.close()
        assert(True)