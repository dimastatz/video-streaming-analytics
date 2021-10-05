FROM openjdk:8 AS build

# Env variables
ENV SCALA_VERSION 2.12.12
ENV SBT_VERSION 1.2.8
# Install Scala
## Piping curl directly in tar
RUN \
curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
echo >> /root/.bashrc && \
echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

# Install sbt
RUN \
#curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
#dpkg -i sbt-$SBT_VERSION.deb && \
#rm sbt-$SBT_VERSION.deb && \
#apt-get update && \
#apt-get install sbt && \
curl -fsL "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" | tar xfz - -C /usr/share && \
chown -R root:root /usr/share/sbt && \
chmod -R 755 /usr/share/sbt && \
ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt && \
sbt sbtVersion && \
mkdir project && \
echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt && \
echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
echo "case object Temp" > Temp.scala && \
sbt compile && \
echo "done with compiling, starting deletion" && \
rm -rf project && \
rm -f build.sbt && \
rm -f Temp.scala && \
rm -rf target && \
echo "done with deletion" && \
mkdir -p /spark/ && \
echo "created spark directory" && \
curl -sL https://archive.apache.org/dist/spark/spark-3.0.1/spark-3.0.1-bin-hadoop3.2.tgz|gunzip| tar x -C /spark/ && \
echo "curled spark" && \
rm -r /spark/spark-3.0.1-bin-hadoop3.2/jars/kubernetes-*-4.1.2.jar || true && \
rm -r /spark/spark-3.0.1-bin-hadoop3.2/jars/kubernetes-*-4.4.2.jar || true && \
echo "starting with wget" && \
wget https://repo1.maven.org/maven2/io/fabric8/kubernetes-model-common/4.9.2/kubernetes-model-common-4.9.2.jar -P /spark/spark-3.0.1-bin-hadoop3.2/jars/ && \
wget https://repo1.maven.org/maven2/io/fabric8/kubernetes-client/4.9.2/kubernetes-client-4.9.2.jar -P /spark/spark-3.0.1-bin-hadoop3.2/jars/ && \
wget https://repo1.maven.org/maven2/io/fabric8/kubernetes-model/4.9.2/kubernetes-model-4.9.2.jar -P /spark/spark-3.0.1-bin-hadoop3.2/jars/ && \
echo "done with wget"

# Define working directory
WORKDIR /opt/input

# Project Definition layers change less often than application code
COPY flumenz/build.sbt ./
WORKDIR /opt/input/project
# COPY project/*.scala ./
COPY flumenz/project/build.properties ./
COPY flumenz/project/*.sbt ./

WORKDIR /opt/input
RUN sbt reload

# Copy rest of application
COPY flumenz ./
RUN sbt testCoverage
RUN SBT_OPTS="-Xms2048M -Xmx2048M -Xss1024M -XX:MaxMetaspaceSize=4096M" sbt 'set test in assembly := {}' clean assembly

FROM openjdk:8-alpine AS spark

# install python
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 && ln -sf python3 /usr/bin/python && apk add curl
RUN python3 -m ensurepip
RUN pip3 install --no-cache --upgrade pip setuptools && pip install pyhocon && pip install kubernetes

ARG spark_home=/spark/spark-3.0.1-bin-hadoop3.2

RUN set -ex && \
    apk upgrade --no-cache && \
    apk add --no-cache bash tini libc6-compat gcompat linux-pam nss && \
    mkdir -p /opt/spark && \
    mkdir -p /opt/spark/work-dir && \
    touch /opt/spark/RELEASE && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd

COPY --from=build ${spark_home}/jars /opt/spark/jars
COPY --from=build ${spark_home}/bin /opt/spark/bin
COPY --from=build ${spark_home}/sbin /opt/spark/sbin
COPY --from=build ${spark_home}/kubernetes/dockerfiles/spark/entrypoint.sh /opt/

FROM spark AS final

RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin

ENV SPARK_HOME /opt/spark
RUN mkdir /opt/spark/conf

COPY flumenz/src/main/resources/*.conf /tmp/

COPY --from=build /opt/input/target/scala-2.12/legion-streaming-assembly-0.2.jar  /opt/spark/jars

WORKDIR /opt/spark/work-dir
RUN wget https://archive.apache.org/dist/kafka/2.2.1/kafka_2.12-2.2.1.tgz

RUN tar -xzf kafka_2.12-2.2.1.tgz
RUN chmod +x /tmp/entrypoint.py

ENTRYPOINT ["python", "/tmp/entrypoint.py"]

