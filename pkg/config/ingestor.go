package config

const (
	DefaultIngestorAPIEndpoint = "127.0.0.1:9000"
	DefaultIngestorAPIInsecure = true
	DefaultBucketName          = "" // we want to let it empty because we can easily abort if it's not configured
	DefaultTempDir             = "/tmp/kubehound"
	DefaultArchiveName         = "archive.tar.gz"
	DefaultMaxArchiveSize      = int64(1 << 30) // 1GB

	IngestorAPIEndpoint = "ingestor.api.endpoint"
	IngestorAPIInsecure = "ingestor.api.insecure"
	IngestorClusterName = "ingestor.cluster_name"
	IngestorRunID       = "ingestor.run_id"
)

type IngestorConfig struct {
	API            IngestorAPIConfig `mapstructure:"api"`
	BucketName     string            `mapstructure:"bucket_name"`
	TempDir        string            `mapstructure:"temp_dir"`
	ArchiveName    string            `mapstructure:"archive_name"`
	MaxArchiveSize int64             `mapstructure:"max_archive_size"`
	ClusterName    string            `mapstructure:"cluster_name"`
	RunID          string            `mapstructure:"run_id"`
}

type IngestorAPIConfig struct {
	Endpoint string `mapstructure:"endpoint" validate:"omitempty,tcp_addr"`
	Insecure bool   `mapstructure:"insecure" validate:"omitempty,boolean"`
}