package main

import (
	"fmt"

	"github.com/DataDog/KubeHound/pkg/cmd"
	"github.com/DataDog/KubeHound/pkg/kubehound/core"
	"github.com/DataDog/KubeHound/pkg/telemetry/log"
	"github.com/spf13/cobra"
)

var (
	cfgFile     = ""
	skipBackend = false
)

var (
	rootCmd = &cobra.Command{
		Use:   "kubehound",
		Short: "A local Kubehound instance",
		Long:  `A local instance of Kubehound - a Kubernetes attack path generator`,
		PreRunE: func(cobraCmd *cobra.Command, args []string) error {
			return cmd.InitializeKubehoundConfig(cobraCmd.Context(), cfgFile, true, false)
		},
		RunE: func(cobraCmd *cobra.Command, args []string) error {
			l := log.Logger(cobraCmd.Context())
			// auto spawning the backend stack
			if !skipBackend {
				err := runBackend(cobraCmd.Context())
				if err != nil {
					return err
				}
			}

			// Passing the Kubehound config from viper
			khCfg, err := cmd.GetConfig()
			if err != nil {
				return fmt.Errorf("get config: %w", err)
			}

			err = core.CoreInitLive(cobraCmd.Context(), khCfg)
			if err != nil {
				return err
			}

			err = core.CoreLive(cobraCmd.Context(), khCfg)
			if err != nil {
				return err
			}

			l.Warn("KubeHound as finished ingesting and building the graph successfully.")
			l.Warn("Please visit the UI to view the graph by clicking the link below:")
			l.Warn("http://localhost:8888")
			// Yes, we should change that :D
			l.Warn("Default password being 'admin'")

			return nil
		},
		SilenceUsage:  true,
		SilenceErrors: true,
	}
)

func init() {
	rootCmd.PersistentFlags().StringVarP(&cfgFile, "config", "c", cfgFile, "application config file")

	rootCmd.PersistentFlags().BoolVar(&skipBackend, "skip-backend", skipBackend, "skip the auto deployment of the backend stack (janusgraph, mongodb, and UI)")

	cmd.InitRootCmd(rootCmd)
}
