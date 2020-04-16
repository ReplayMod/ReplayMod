pipeline {
    agent {
        docker { image 'openjdk:8-jdk' }
    }
    stages {
        stage('Build') {
            environment {
                GRADLE_USER_HOME = '.gradle/user_home'
            }
            steps {
                cache(maxCacheSize: 4096, caches: [
                        [$class: 'ArbitraryFileCache', excludes: 'modules-2/modules-2.lock,*/plugin-resolution/**', includes: '**/*', path: '.gradle/user_home/caches'],
                        [$class: 'ArbitraryFileCache', excludes: '', includes: '**/*', path: '.gradle/user_home/wrapper'],
                        [$class: 'ArbitraryFileCache', excludes: '', includes: '**/*', path: '.gradle/loom-cache'],
                ]) {
                    sh './gradlew :jGui:1.7.10:setupCIWorkspace :1.7.10:setupCIWorkspace'
                    sh './gradlew --parallel'
                }
                archiveArtifacts 'versions/*/build/libs/*.jar'
            }
        }
        stage('Deploy') {
            steps {
                withAWS(endpointUrl: 'https://minio.johni0702.de', credentials: 'minio') {
                    s3Upload bucket: 'replaymod', includePathPattern: '*.jar', workingDir: 'build/libs', acl: 'PublicRead', pathStyleAccessEnabled: true
                }
            }
        }
    }
}
