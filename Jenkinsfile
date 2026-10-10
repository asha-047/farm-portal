pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target environment')
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        IMAGE = 'asha047/farm-portal'
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
        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest .'
            }
        }
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh 'echo $DH_PASS | docker login -u $DH_USER --password-stdin'
                    sh 'docker push $IMAGE:$BUILD_NUMBER'
                    sh 'docker push $IMAGE:latest'
                }
            }
        }
        stage('Run Container') {
            steps {
                sh 'docker rm -f farm || true'
                sh 'docker run -d --name farm -p 8083:8081 $IMAGE:$BUILD_NUMBER'
                sh '''
                    for i in $(seq 1 12); do
                      curl -sf http://localhost:8083/produce > /dev/null && echo "Health check passed" && exit 0
                      sleep 5
                    done
                    echo "Health check failed"; exit 1
                '''
            }
        }
    }
}