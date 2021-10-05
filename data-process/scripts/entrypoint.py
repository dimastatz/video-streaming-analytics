import os
import sys
import time
import json
import shutil
import logging
import subprocess
from pyhocon import ConfigFactory


logging.basicConfig(stream=sys.stdout, level=logging.INFO)


def create_cmd(conf: ConfigFactory, env: str) -> str:
    return '{} {} {} {} {} {} {}'.format(
        '/opt/spark/bin/spark-submit', 
        ' '.join(conf.get('legion-streaming.spark.args')), 
        ' '.join(conf.get('legion-streaming.spark.conf')), 
        ' '.join(conf.get('legion-streaming.spark.kubernetes')),
        ' '.join(get_secrets(os.environ)),
        conf.get('legion-streaming.spark.assembly'), env)


def get_secrets(env: dict):
    return [        
        '--conf spark.kubernetes.driverEnv.MONGO_USER={}'.format(env['MONGO_USER']),
        '--conf spark.kubernetes.driverEnv.MONGO_SECRET={}'.format(env['MONGO_SECRET'])
    ]

def load_conf(env: str):
    path = '/tmp/{}.conf'.format(env)
    conf = ConfigFactory.parse_file(path)
    return conf
    

def resolve_env(env: dict = os.environ):
    hostname: str = env.get('HOSTNAME', '')
    envs = {'-dev': 'dev', '-staging': 'stage', '-production': 'prod'}
    return next((x[1] for x in envs.items() if x[0] in hostname), 'local')
    

def replace_in_file(fname, seacrh, replace):
    with open(fname, "rt") as fin:
        data = fin.read().replace(seacrh, replace)
    with open(fname, "wt") as fout:
        fout.write(data)


def get_token(fname = '/var/run/secrets/kubernetes.io/serviceaccount/token'):
    with open(fname, 'r') as f:
        return f.read()


def get_running_pods(host, namespace):
    try:
        from kubernetes import client, config
        conf = client.Configuration()
        conf.host = host
        conf.verify_ssl = False
        conf.api_key["authorization"] = get_token()
        conf.api_key_prefix['authorization'] = 'Bearer'

        api = client.CoreV1Api(client.ApiClient(conf))
        res = api.list_namespaced_pod(namespace=namespace, watch=False)
        return [(pod.metadata.name, pod.status.phase) for pod in res.items]
    except Exception as e:
        logging.warn('failed to obtain running pods {}'.format(e))
        return []


def get_pods(ns):
    pods = subprocess.getoutput('kubectl get pods -n={}'.format(ns))
    terminated = [x for x in pods.split('\n') if 'Killed' in x or 'Completed' in x] 
    return (pods, terminated)


def monitor_pods(master, ns, wait_intreval=60):
    restart_needed = False

    while True:
        time.sleep(wait_intreval)
        (pods, terminated) = get_pods(ns)
        logging.info(pods, terminated)

        if bool(terminated) and restart_needed:
            logging.info(subprocess.getoutput('kubectl delete --all pods -n={}'.format(ns)))
            restart_needed = False
        else:
            restart_needed = bool(terminated)
        

if __name__ == "__main__":
    logging.info('running boot: {}'.format(str(sys.argv)))
    replace_in_file('/opt/entrypoint.sh', '/usr/bin/tini', '/sbin/tini')

    if not os.environ.get('SPARK_USER', None):
        env = resolve_env()
        conf = load_conf(env)
        cmd = create_cmd(conf, env)
        ns = '-'.join(os.environ['HOSTNAME'].split('-')[:3])

        args = conf.get('legion-streaming.spark.args')
        master = [x.split(' ')[1] for x in args if 'master' in x][0]
        master = master.split('k8s://')[1] if 'k8s://' in master else master
        
        logging.info('obtaining pods {} {}'.format(master, ns))
        pods = get_running_pods(master, ns)

        logging.info('running envs {} {}'.format(master, ns))
        logging.info('running spark submit: {}'.format(cmd))
        os.system(cmd)
        monitor_pods(master, ns)
    else: 
        args = ' '.join([str(x) for x in sys.argv[1:]])
        logging.info('running default script: {}'.format(args))
        os.system('/opt/entrypoint.sh {}'.format(args))
