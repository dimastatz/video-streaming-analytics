from typing import List
from cdn_origin.origin import Origin

class OriginSet:
    def __init__(self, origins: List[Origin]):
        self.origins: List[Origin] = origins

    def upload(self, dest_name: str, data: bytes, job_types: str, info: None):
        [o.upload(dest_name, data, job_types, info) for o in self.origins]
        
    def wait(self, timeout: float):
        next(o.wait(timeout) for o in self.origins)

    def close(self):
        [o.shutdown() for o in self.origins]