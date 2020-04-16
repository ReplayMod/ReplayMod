pipeline {
    agent {
        docker { image 'openjdk:8-jdk' }
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew :jGui:1.7.10:setupCIWorkspace :1.7.10:setupCIWorkspace'
                sh './gradlew --parallel'
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
