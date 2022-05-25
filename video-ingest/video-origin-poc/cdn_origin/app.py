import time
from flask import Flask, request


app = Flask(__name__)


@app.route('/')
def health():
    return f'Health check {time.time()}'


@app.route('/frame/<beam_id>/<slice_id>/<frame_id>', methods=['POST']) 
def upload(beam_id, slice_id, frame_id):
    for f in  request.files:
        print(f[0])
    return 'Success'