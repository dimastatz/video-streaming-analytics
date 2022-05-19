import os
import slicer
import encoder
import gateway
import logging


if __name__ == "__main__":
    role = os.environ.get('ROLE', 'gateway')
    if role == 'gateway':
        gateway.main()
    elif role == 'encoder':
        slicer.main()
    elif role == 'slicer':
        encoder.main()
    else:
        logging.error(f'Unknown role {role}') 
    