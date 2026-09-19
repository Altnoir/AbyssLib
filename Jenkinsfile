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
                withCredentials([
                    usernamePassword(credentialsId: 'JENKINS_MAVEN', usernameVariable: 'MAVEN_USERNAME', passwordVariable: 'MAVEN_PASSWORD')
                ]) {
                    echo 'Building project'
                    // RELEASE=true：发布稳定的 <mod_version>（1.0.0），消费方按这个坐标依赖。
                    // 去掉它则发布 <mod_version>-<BUILD_NUMBER> 快照（1.0.0-17 这种，可追溯）。
                    sh 'RELEASE=true ./gradlew build publish'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
        }
    }
}
