#!groovy

// Important
// Remember to ensure that the Project version information is on top of the pom.xml file because
// the getVersionFromPom will attempt to read the version information that it encounter at the
// first occurance.

node('maven') {

  def mvnCmd = "mvn -s ./nexus_openshift_settings.xml"
  def nexusReleaseURL = "http://nexus3:8081/repository/releases"
  def activeSvc = ""
  def targetSvc = ''
  def devProjectName = 'dev'
  def prodProjectName = 'prod'
  def testProjectName = 'test'
  def devImageNameN = "dev"     // image namespace
  def testImageNameN = "dev"    // image namespace
  def prodImageNameN = "dev"    // image namespace
  def wildcardDNS = ".ocp.demo.com"
  
  
  stage('Checkout Source') {
    checkout scm
  }

  // In order to access to pom.xml, these variables and method calls must be placed after checkout scm.
  def groupId    = getGroupIdFromPom("pom.xml")
  def artifactId = getArtifactIdFromPom("pom.xml")
  def version    = getVersionFromPom("pom.xml")
  def packageName = getGeneratedPackageName(groupId, artifactId, version)
  def devImageName = "nationalparks:DevelopmentReady-$version"
  def testImageName = "nationalparks:TestReady-$version"
  def prodImageName = "nationalparks:ProdReady-$version"
  
    
  stage('Build jar') {
    sh "${mvnCmd} package -DskipTests=true"
  }

  stage('Unit Tests') {
    // TBD
    //-- sh "${mvnCmd} test"
  }

  stage('Code Analysis') {
    // TBD
    sh "${mvnCmd} jacoco:report sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -DskipTests=true"
  }

  stage('Publish jar to Nexus') {
    echo "Publish jar file to Nexus..."
    sh "${mvnCmd} deploy -DskipTests=true -DaltDeploymentRepository=nexus::default::${nexusReleaseURL}"
    echo "Generated jar file: ${packageName}"
  }

  stage('Build OpenShift Image') {
    echo "Start building OCP ..."
    echo "Tag the DC and BC ..."
    
    sh "oc project $devProjectName && oc patch dc nationalparks --patch '{\"spec\": { \"triggers\": [ { \"type\": \"ImageChange\", \"imageChangeParams\": { \"automatic\": true, \"containerNames\": [ \"nationalparks\" ], \"from\": { \"kind\": \"ImageStreamTag\", \"namespace\": \"$devImageNameN\", \"name\": \"$devImageName\"}}}]}}' -n $devProjectName"
    sh "oc project $devProjectName && oc patch bc nationalparks --patch '{\"spec\":{\"output\": {\"to\": {\"kind\": \"ImageStreamTag\", \"name\":\"$devImageName\"}}}}' -n $devProjectName"
    sh "mkdir ./deployments"
    sh "oc project $devProjectName && curl -o ./deployments/nationalparks.jar http://nexus3:8081/repository/releases/${packageName}"
    sh "oc start-build nationalparks --from-dir=. -n $devProjectName --wait=true"
    
    // need to explicitly rollout because there is no ConfigChange trigger enabled
    //--sh "oc rollout latest dc/nationalparks -n dev"
    
    openshiftVerifyBuild("namespace": "$devProjectName", "buildConfig": "nationalparks")
    openshiftVerifyService("namespace": "$devProjectName", "serviceName": "nationalparks")
    
    verifyNationalparksDB("http://nationalparks-devProjectName$wildcardDNS/ws/data/all/")
    
  }
  
  stage ('Deploy to Test Env'){
    echo "Tag the TEST DC"
    sh "oc project $testProjectName && oc patch dc nationalparks --patch '{\"spec\": { \"triggers\": [ { \"type\": \"ImageChange\", \"imageChangeParams\": { \"automatic\": true, \"containerNames\": [ \"nationalparks\" ], \"from\": { \"kind\": \"ImageStreamTag\", \"namespace\": \"$testImageNameN\", \"name\": \"$testImageName\"}}}]}}' -n $testProjectName"
    echo "Tag TestReady Image Stream... which will trigger Test Deployment."
    sh "oc project $devProjectName && oc tag $devImageNameN/$devImageName $testImageNameN/$testImageName"
    
    // need to explicitly rollout because there is no ConfigChange trigger enabled
    //-- sh "oc rollout latest dc/nationalparks -n $testProjectName"
    
    openshiftVerifyDeployment("namespace": "$testProjectName", "deploymentConfig": "nationalparks", "replicaCount": 1, "verifyReplicaCount": 1, "waitTime": 240000)
    openshiftVerifyService("namespace": "$testProjectName", "serviceName": "nationalparks")
    
    verifyNationalparksDB('http://nationalparks-testProjectName$wildcardDNS/ws/data/all/')
  }

  stage('Integration Test') {
    // TBD
    
    //def count = sh script: 'curl -v --silent http://nationalparks-dev.ocp.demo.com/ws/data/all/ 2>&1 | grep -Eoi "Hwange National Park Airport" | wc -l', returnStatus: true
    
    //if (count == 0) {
    //    
    // }
    
  }

  // Blue/Green Deployment into Production
  stage('Deploy new Version') {
    
    String count = sh script: 'oc get route nationalparks-bluegreen -n $prodProjectName | grep nationalparks-green  | wc -l | tr -d \"\n\"', returnStdout: true
    // echo "count = '$count'"
    
    
    if (count == "1"){      // Green is active on the route
        echo "nationalparks-green is active."
        activeSvc = 'nationalparks-green'
        targetSvc = 'nationalparks-blue'
    }
    else{                   // Blue is active on the route
        echo "nationalparks-blue is active."
        activeSvc = 'nationalparks-blue'
        targetSvc = 'nationalparks-green'
    }
    
    sh "oc project $prodProjectName && oc patch dc $targetSvc --patch '{\"spec\": { \"triggers\": [ { \"type\": \"ImageChange\", \"imageChangeParams\": { \"automatic\": true, \"containerNames\": [ \"$targetSvc\" ], \"from\": { \"kind\": \"ImageStreamTag\", \"namespace\": \"$prodImageNameN\", \"name\": \"$prodImageName\"}}}]}}' -n $prodProjectName"
    sh "oc project $prodProjectName && oc tag $testImageNameN/$testImageName $prodImageNameN/$prodImageName"
    
    // need to explicitly call rollout command because the image trigger will not be activated since we keep changing the image trigger attributes without specifying ConfigChange trigger in the very first place, thus any changes to trigger will not be captured.
    // Not very stable, all new projects seems working fine by image trigger.
    //--sh "oc rollout latest dc/$targetSvc -n $prodProjectName"
    
    openshiftVerifyDeployment("namespace": "$prodProjectName", "deploymentConfig": "$targetSvc", "replicaCount": 1, "verifyReplicaCount": 1, "waitTime": 240000)
    openshiftVerifyService("namespace": "$prodProjectName", "serviceName": "$targetSvc")
    verifyNationalparksDB("http://$targetSvc:8080/ws/data/all/")
    
  }
  
  
  stage('Switch over to new Version') {
    
    input "Switch Production?"
    if (activeSvc == "nationalparks-blue"){
        sh "oc label service $targetSvc type=parksmap-backend -n $prodProjectName --overwrite=true"
        sh "oc label service $activeSvc type- -n $prodProjectName"
        sh "oc patch route/nationalparks-bluegreen -p \'{\"spec\":{\"to\":{\"name\":\"nationalparks-green\"}}}\' -n $prodProjectName"
    }
    else{
        sh "oc label service $targetSvc type=parksmap-backend -n $prodProjectName --overwrite=true"
        sh "oc label service $activeSvc type- -n $prodProjectName"
        sh "oc patch route/nationalparks-bluegreen -p \'{\"spec\":{\"to\":{\"name\":\"nationalparks-blue\"}}}\' -n $prodProjectName"
    }
    
    sh "oc project $prodProjectName && oc patch dc nationalparks-green --patch '{\"spec\": { \"triggers\": []}}' -n $prodProjectName"
    sh "oc project $prodProjectName && oc patch dc nationalparks-blue --patch '{\"spec\": { \"triggers\": []}}' -n $prodProjectName"
    
  }
}

// Convenience Functions to read variables from the pom.xml
// Do not change anything below this line.
def getVersionFromPom(pom) {
  def matcher = readFile(pom) =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}
def getGroupIdFromPom(pom) {
  def matcher = readFile(pom) =~ '<groupId>(.+)</groupId>'
  matcher ? matcher[0][1] : null
}
def getArtifactIdFromPom(pom) {
  def matcher = readFile(pom) =~ '<artifactId>(.+)</artifactId>'
  matcher ? matcher[0][1] : null
}

def getGeneratedPackageName(groupId, artifactId, version){
    String warFileName = "${groupId}.${artifactId}"
    warFileName = warFileName.replace('.', '/')
    "${warFileName}/${version}/${artifactId}-${version}.jar"
}

def verifyNationalparksDB(url){
    echo "URL: $url"
    def count = sh script: "curl -s $url | grep \"Hwange National Park Airport\" | wc -l | tr -d \"\n\"", returnStatus: true
    if (count == "0") {
        error("Build failed: nationalparks does not return expected query result. Please check database connection.")
    }
}
