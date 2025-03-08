FROM lr-acr-registry.cn-shanghai.cr.aliyuncs.com/devops-apac-mgmt/spark-3:3.3.0
USER root
ENV WORK_PWD=/opt
WORKDIR $WORK_PWD/
COPY ./target/livy-ramp.jar $WORK_PWD/app.jar
COPY ./ext/db/job_history.db $WORK_PWD/job_history.db
RUN mkdir -p $WORK_PWD/config
ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
