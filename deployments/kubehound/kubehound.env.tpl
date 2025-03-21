# Custom config for docker compose environment
KH_MONGODB_URL=mongodb://mongodb:27017
KH_JANUSGRAPH_URL=ws://kubegraph:8182/gremlin
# Default config
KH_INGESTOR_API_ENDPOINT=0.0.0.0:9000
KH_INGESTOR_TEMP_DIR=/tmp/kubehound
KH_INGESTOR_MAX_ARCHIVE_SIZE=2147483648 # 2GB
KH_INGESTOR_ARCHIVE_NAME=archive.tar.gz
KH_LOG_FORMAT=dd
# AWS Bucket configuration
KH_INGESTOR_REGION=us-east-1
KH_INGESTOR_BUCKET_URL="" # s3://<your_bucket>
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_SESSION_TOKEN= # for aws-vault generated credentials
