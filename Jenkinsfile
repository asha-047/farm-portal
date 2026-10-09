pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target environment')
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build') {
            steps { sh 'mvn -B clean package -DskipTests' }
        }
        stage('Test') {
            steps { sh 'mvn -B test' }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true
                }
            }
        }
        stage('Package') {
            steps { archiveArtifacts artifacts: 'target/*.war', fingerprint: true }
        }
        stage('Deploy to Tomcat') {
            steps {
                sh 'sudo cp target/farm.war /var/lib/tomcat10/webapps/farm-${ENVIRONMENT}.war'
                echo "Deployed to environment: ${params.ENVIRONMENT}"
            }
        }
    }
}