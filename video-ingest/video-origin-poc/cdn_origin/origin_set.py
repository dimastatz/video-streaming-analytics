import threading
from typing import List
from cdn_origin.origin import Origin

class OriginSet:
    def __init__(self, origins: List[Origin]):
        self.trigger = threading.Event()
        self.origins: List[Origin] = origins
        [o.add_all_done_callback(lambda: self.trigger.set()) for o in origins]
        
    def upload(self, dest_name: str, data: bytes, job_types: str, info: None):
        [o.upload(dest_name, data, job_types, info) for o in self.origins]
        
    def wait(self, timeout: float) -> bool:
        return self.trigger.wait(timeout)
        
    def close(self):
        [o.close() for o in self.origins]