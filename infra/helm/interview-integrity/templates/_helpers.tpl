{{/*
Full name for a resource. Service name is used as-is; global resources use the
release name.
*/}}
{{- define "integrity.fullname" -}}
{{- if .Values.global.nameOverride -}}
{{- .Values.global.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Service short name (dash-form), e.g. "api-gateway".
*/}}
{{- define "integrity.serviceName" -}}
{{- printf "%s" .name -}}
{{- end -}}

{{/*
Kubernetes label set for a service.
*/}}
{{- define "integrity.labels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: integrity-pro
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}

{{/*
Selector labels for a service.
*/}}
{{- define "integrity.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Image reference for a service.
*/}}
{{- define "integrity.image" -}}
{{- printf "%s/%s:%s" .Values.global.image.registry .image .Values.global.image.tag -}}
{{- end -}}

{{/*
Prometheus scrape annotations when monitoring is enabled.
*/}}
{{- define "integrity.monitoringAnnotations" -}}
{{- if .Values.global.monitoring.enabled -}}
prometheus.io/scrape: "true"
prometheus.io/port: {{ .port | quote }}
prometheus.io/path: /actuator/prometheus
{{- end -}}
{{- end -}}

{{/*
Shared environment block injected into every service container. DB_NAME and
SERVER_PORT are per-service and appended by the deployment template.
*/}}
{{- define "integrity.commonEnv" -}}
- name: SPRING_PROFILES_ACTIVE
  value: {{ .Values.global.spring.profile | quote }}
- name: SPRING_CONFIG_LOCATION
  value: {{ .Values.global.spring.configLocation | quote }}
- name: DB_HOST
  value: {{ .Values.global.dataPlane.db.host | quote }}
- name: DB_PORT
  value: {{ .Values.global.dataPlane.db.port | quote }}
- name: DB_USERNAME
  value: {{ .Values.global.dataPlane.db.username | quote }}
- name: REDIS_HOST
  value: {{ .Values.global.dataPlane.redis.host | quote }}
- name: REDIS_PORT
  value: {{ .Values.global.dataPlane.redis.port | quote }}
- name: KAFKA_BOOTSTRAP_SERVERS
  value: {{ .Values.global.dataPlane.kafka.bootstrapServers | quote }}
- name: KAFKA_SASL_ENABLED
  value: {{ .Values.global.dataPlane.kafka.saslEnabled | quote }}
- name: KAFKA_SASL_USERNAME
  value: {{ .Values.global.dataPlane.kafka.saslUsername | quote }}
- name: MAIL_HOST
  value: {{ .Values.global.dataPlane.mail.host | quote }}
- name: MAIL_PORT
  value: {{ .Values.global.dataPlane.mail.port | quote }}
- name: MAIL_FROM
  value: {{ .Values.global.dataPlane.mail.from | quote }}
- name: PLATFORM_STORAGE_ENDPOINT
  value: {{ .Values.global.dataPlane.storage.endpoint | quote }}
- name: PLATFORM_STORAGE_REGION
  value: {{ .Values.global.dataPlane.storage.region | quote }}
- name: EUREKA_SERVER_URL
  value: {{ .Values.global.dataPlane.eureka.url | quote }}
- name: APP_TIMEZONE
  value: {{ .Values.global.platform.timezone | quote }}
- name: FRONTEND_BASE_URL
  value: {{ .Values.global.platform.frontendBaseUrl | quote }}
- name: APP_NAME
  value: {{ .Values.global.app.name | quote }}
- name: JAVA_TOOL_OPTIONS
  value: {{ printf "%s %s" .Values.global.jvm.heapInitial .Values.global.jvm.heapMax | quote }}
{{- end -}}

{{/*
Name of the ConfigMap holding the externalised Spring configuration.
*/}}
{{- define "integrity.configMap" -}}
{{- .Values.global.config.configMapName -}}
{{- end -}}

{{/*
Name of the Secret holding platform credentials.
*/}}
{{- define "integrity.secrets" -}}
{{- .Values.global.secrets.name -}}
{{- end -}}
