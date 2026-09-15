{{- define "novabank.name" -}}novabank{{- end -}}
{{- define "novabank.labels" -}}
app.kubernetes.io/part-of: novabank
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}
