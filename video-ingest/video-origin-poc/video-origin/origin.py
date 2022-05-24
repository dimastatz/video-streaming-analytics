from queue import Queue
from typing import Callable
from concurrent.futures import ThreadPoolExecutor, Future, wait


class Origin:
    def __init__(self, upload_handler: Callable, workers: int = 16) -> None:
        self.results = Queue()
        self.upload_handler = upload_handler
        self.pool = ThreadPoolExecutor(workers)
        
    def upload(self, dest_name: str, data: bytes, job_types: str, info: None):
        future: Future = self.pool.submit(self.upload_handler, dest_name, data, job_types, info)
        future.add_done_callback(self.all_done_callback)
        self.results.put(future)
        
    def wait(self, timeout: float):
        features = list(self.results)
        result = wait(features, timeout=timeout)
        return result

    def all_done_callback(self, handler: Callable):
        has_running_tasks = any(f for f in list(self.results) if f.running())
        if not has_running_tasks:
            handler()

    def close(self):
        self.pool.shutdown()