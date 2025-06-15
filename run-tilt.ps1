# This script loads environment variables from docker/.env and runs 'tilt up'
Get-Content docker/.env | ForEach-Object {
    if ($_ -match '^(.*?)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
tilt up 