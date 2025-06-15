# Tiltfile

docker_build(
    'comp47910-bookstore',
    '.',
    dockerfile='Dockerfile',
    live_update=[
        sync('./src', '/app/src'),
        sync('./pom.xml', '/app/pom.xml'),
        sync('./target', '/app/target'), 
        run('mvn package -DskipTests', trigger=['./src', './pom.xml']),
        restart_container(),
    ]
)

docker_compose(['./docker/docker-compose.yml', './docker/docker-compose-app.yml'])

dc_resource('mysql', labels=['database'])
dc_resource('app', labels=['application'])
