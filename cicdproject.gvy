pipeline {
    agent any
    stages {
        stage('compile') {
	         steps {
                // step1 
                echo 'compiling..'
		            git url: 'https://github.com/aiswaryacs01/PetClinic'
		            sh script: 'mvn compile'
           }
        }
        stage('codereview-pmd') {
	         steps {
                // step2
                echo 'codereview..'
		            sh script: 'mvn -P metrics pmd:pmd'
           }
	         post {
               success {
		             recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
               }
           }		
        }
        stage('unit-test') {
	          steps {
                // step3
                echo 'unittest..'
	               sh script: 'mvn test'
            }
	          post {
               success {
                   junit 'target/surefire-reports/*.xml'
               }
            }			
        }
        stage('codecoverage') {

           tools {
              jdk 'jdk1.8'
           }
	         steps {
                // step4
                echo 'codecoverage..'
		            sh script: 'mvn cobertura:cobertura -Dcobertura.report.format=xml'
           }
	         post {
               success {
	               cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: 'target/site/cobertura/coverage.xml', conditionalCoverageTargets: '70, 0, 0', failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false                  
               }
           }		
        }
        stage('package/build-war') {
	         steps {
                // step5
                echo 'package......'
		            sh script: 'mvn package'	
           }		
        }
        stage('build & push docker image') {
	         steps {
              withDockerRegistry(credentialsId: 'Login_Dockerhub', url: 'https://index.docker.io/v1/') {
                    sh script: 'cd  $WORKSPACE'
                    sh script: 'docker build --file Dockerfile --tag docker.io/aiswaryacs01/petclinic:$BUILD_NUMBER .'
                    sh script: 'docker push docker.io/aiswaryacs01/petclinic:$BUILD_NUMBER'
              }	
           }		
        }
    stage('Deploy-App-QA') {
  	   steps {
              sh script:export ANSIBLE_HOST_KEY_CHECKING=FALSE     
              sh 'ansible-playbook --inventory /tmp/inv $WORKSPACE/deploy-kube.yml --extra-vars "env=QA build=$BUILD_NUMBER"'
	   }
	   post { 
              always { 
                cleanWs() 
	      }
	   }
	}
    }
}
