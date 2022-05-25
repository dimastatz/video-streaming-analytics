import boto3


class UploaderS3:
    def __init__(self, bucket: str):
        self.bucket = bucket

    def upload(self, dest_name: str, data: bytes, job_types: str, info: None):
        client = boto3.client('s3')
        client.put_object(Body=data, Bucket=self.bucket, Key=dest_name, ContentType=job_types)
        
