pipeline {
    agent any
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target environment')
    }
    environment {
        IMAGE = 'YOUR_DOCKERHUB_USER/farm-portal'
    }
    stages {
        stage('Checkout') {                       // Task 8
            steps { checkout scm }
        }
        stage('Build') {                          // Task 8
            steps { sh 'mvn -B clean package -DskipTests' }
        }
        stage('Test') {                           // Task 10
            steps { sh 'mvn -B test' }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true
                }
            }
        }
        stage('Package') {                        // Task 8
            steps { archiveArtifacts artifacts: 'target/*.war' }
        }
        stage('Deploy to Tomcat') {               // Task 8
            steps {
                sh 'sudo cp target/farm.war /var/lib/tomcat10/webapps/farm-${ENVIRONMENT}.war'
            }
        }
        stage('Docker Build') {                   // Task 12
            steps { sh 'docker build -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest .' }
        }
        stage('Docker Push') {                    // Task 12
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh 'echo $DH_PASS | docker login -u $DH_USER --password-stdin'
                    sh 'docker push $IMAGE:$BUILD_NUMBER'
                    sh 'docker push $IMAGE:latest'
                }
            }
        }
        stage('Run Container') {                  // Task 12
            steps {
                sh 'docker rm -f farm || true'
                sh 'docker run -d --name farm -p 8083:8081 $IMAGE:$BUILD_NUMBER'
            }
        }
    }
}
