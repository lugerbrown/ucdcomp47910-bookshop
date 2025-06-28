
config.define_string_list("to-run", args=True)
cfg = config.parse()
to_run = cfg.get('to-run', ['dependencies'])

 #trigger_mode(TRIGGER_MODE_MANUAL)

def application_sync():
    docker_build(
        'comp47910-bookshop',
        '.',
        dockerfile='Dockerfile',
        live_update=[
            sync('./src/main/resources/templates', '/app/src/main/resources/templates'),
            sync('./src/main/resources/static', '/app/src/main/resources/static'),

            sync('./src/main/java', '/app/src/main/java'),
            sync('./pom.xml', '/app/pom.xml'),
            run('mvn compile -DskipTests', trigger=['./src/main/java', './pom.xml']),
            restart_container(),
        ]
    )
    docker_compose(['./docker/docker-compose-app.yml'])
    dc_resource('app', labels=['application'])

def load_dependencies():
    docker_compose(['./docker/docker-compose.yml'])
    dc_resource('mysql', labels=['database'])

if 'dependencies' in to_run:
    load_dependencies()

if 'app-sync' in to_run:
    application_sync()
