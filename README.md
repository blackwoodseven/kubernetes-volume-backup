# Kubernetes-Volume-Backup
This project is an attempt at making a backup solution for Kubernetes Persistent Volume Claims.

Currently it only supports pushing backups to Amazon S3, but the architecture is meant to be pluggable, so it could be extended to handle other targets.

**ATTENTION:** We strongly recommend that you enable versioning on your S3 bucket, so you will be able to retrieve overwritten versions of files.

## Docker image
The docker image for this project is available here:

[![](https://img.shields.io/docker/pulls/blackwoodseven/kubernetes-volume-backup.svg)](https://hub.docker.com/r/blackwoodseven/kubernetes-volume-backup/)

## Usage
To us this project for backing up your volumes, you must run it as a side-car in the pod which is using the volume. You will also have to create a AWS IAM Policy to allow the container to push backups to S3

Example IAM Policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListAllMyBuckets"
            ],
            "Resource": "arn:aws:s3:::*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:PutObject"
            ],
            "Resource": "arn:aws:s3:::bw7-k8s-dev-backup"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:PutObject"
            ],
            "Resource": "arn:aws:s3:::bw7-k8s-dev-backup/*"
        }
    ]
}
```

Once you have your configuration you can deploy a backup container as a side-car with access to the volume you want to backup. For example if you have this deployment:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
        volumeMounts:
        - name: some-volume
          path: /some/volume
      volumes:
      - name: some-volume
        persistentVolumeClaim:
          claimName: nginx-volume
```

You can add this backup solution in like this:

```yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.7.9
        ports:
        - containerPort: 80
        volumeMounts:
        - name: some-volume
          path: /some/volume
      - name: volume-backup
        image: blackwoodseven/kubernetes-volume-backup:latest
        volumeMounts:
        - name: some-volume
          path: /some/volume
        env:
        - name: BACKUP_INTERVAL
          value: PT1H
        - name: K8S_CONTAINER_NAME
          value: volume-backup
        - name: K8S_API_HOSTNAME
          value: kubernetes.default
        - name: K8S_POD_NAME
          valueFrom:
            fieldRef:
              apiVersion: v1
              fieldPath: metadata.name
        - name: K8S_NAMESPACE
          valueFrom:
            fieldRef:
              apiVersion: v1
              fieldPath: metadata.namespace
        - name: AWS_DEFAULT_REGION
          value: eu-west-1
        - name: AWS_S3_BUCKET_NAME
          value: your-backup-s3-bucket
        - name: AWS_ACCESS_KEY_ID
          valueFrom:
            secretKeyRef:
              key: aws-access-key-id
              name: backup-s3-access-key
        - name: AWS_SECRET_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              key: aws-secret-access-key
              name: backup-s3-access-key
      volumes:
      - name: some-volume
        persistentVolumeClaim:
          claimName: nginx-volume
```

* The AWS keys  in this example are stored in a Kubernetes Secret, but you could specify them directly if you prefer that.
* The Pod Name and Namespace is supplied by Kubernetes' Downward API.
* The `BACKUP_INTERVAL` must be specified as an ISO 8601 Duration (https://en.wikipedia.org/wiki/ISO_8601#Durations).

The backups will be added to your S3 bucket, under the `<namespace>/<persistent-volume-claim-name>` path, and will be stored in the same file structure as the actual volume.

## Bugs or issues?
There's probably a lot of issues we haven't run into yet, but if you do, please file an issue here on Github, and we will try to help you out.

## Development
Currently it's not possible to try out the project outside of Kubernetes, but you can execute unittests using:

    docker-compose build
    docker-compose run test
