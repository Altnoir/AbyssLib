#!/usr/bin/env groovy

pipeline {
    agent any

    tools {
        jdk "jdk-25"
    }

    stages {
        stage('Setup') {
            steps {
                echo 'Setup Project'
                sh 'chmod +x gradlew'
                sh './gradlew clean'
            }
        }

        stage('Build') {
            steps {
                echo 'Building project'
                sh './gradlew build'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
        }
    }
}
