pipeline {
    agent any
    triggers {
        pollSCM('') // Enabling being build on Push
    }
    environment {
        String IMAGE_TAG = BRANCH_NAME.replace("/", "_")
    }
    stages {
        stage('Initialize') {
            steps {
                sh 'echo "Initialize"'
            }
        }

        stage('test') {
            steps {
                sh 'echo "ignore test for temporary"'
            }
        }

        stage('build docker image') {
            when {
                anyOf {
                    branch 'feature/subscriber-feed'
                }
            }
            steps {
                node('Node_5.68') {
                    sh 'echo Start building'
                    sh "echo pull code from branch ${BRANCH_NAME}"
                    git 'ssh://git@hub.com'
                    sh "git checkout ${BRANCH_NAME}"
                    sh "git pull origin ${BRANCH_NAME}"
                    sh "docker build -t image_name ."
                    sh "docker tag image_name hub-user/repo-name:${IMAGE_TAG}"
                    sh "docker push hub-user/repo-name:${IMAGE_TAG}"
                }
            }
            post {
                success {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [FINISH] - Java build stage")
                }
                unstable {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [UNSTABLE] - Java build stage")
                }
                failure {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [FAILURE] - Java build stage")
                }
            }
        }

        stage('deploy product') {
            when {
                branch "feature/subscriber-feed"
            }
            agent {
                node {
                    label 'node_name'
                    customWorkspace 'worker_dir'
                }
            }
            environment {
                String IMAGE_TAG = BRANCH_NAME.replace("/", "_")
                String RUN_DIR = 'run_dir'
            }
            steps {
                sh 'echo Start deploy on node_name'
                sh "echo ${BRANCH_NAME}"
                sh "docker pull hub-user/repo-name:${IMAGE_TAG}"
                deploy()
            }

            post {
                success {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [FINISH] - Deploy stage")
                }
                unstable {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [UNSTABLE] - Deploy stage")
                }
                failure {
                    notifyTelegram("Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' - [FAILURE] - Deploy stage")
                }
            }
        }

        stage('finish') {
            agent { label 'master' }
            steps {
                sh 'echo "Finish"'
            }
        }
    }
    post {
        always {
            notifyTelegram("[FINISH] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
            //deleteDir() /* clean up our workspace */
        }
        success {
            notifyTelegram("[SUCCESS] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
        }
        unstable {
            notifyTelegram("[UNSTABLE] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
        }
        failure {
            notifyTelegram("[FAILURE] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
        }
        changed {
            notifyTelegram("[CHANGE] Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'")
        }
    }
}

void deploy() {
    sh 'echo "deploying..."'
    sh 'docker-compose up -d --force-recreate container_name'

    sh 'docker system prune -f || exit 0'
    sh 'sleep 1s'
}

def notifyTelegram(String message) {
    //send message to telegram
}
