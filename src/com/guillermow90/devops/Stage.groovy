package com.guillermow90.devops

class Stage {

	static String parsedProjectName = null
	static String parsedProjectVersion = null
	static def projectImage = null


	static def replacePlaceholder(script, filename, placeholder, value) {
		script.sh """set +x
			sed 's`{{ *${placeholder} *}}`${value}`g' -i "${filename}"
		"""
	}


	static def waitForOK(script, seconds, callback) {
		def ok = true
		try {
			script.timeout(time: seconds, unit:'SECONDS') {
				script.input(message: "${script.STAGE_NAME}", ok: "Ejecutar")
			}
		} catch(e) {
			ok = false
		} finally {
			if(ok) {
				callback.call()
			}
		}
	}


	static def parseProjectParameters(script, projectFile) {
		if(projectFile == 'pom.xml') {
			def name = script.sh(
				script: "xmllint --xpath \"/*[local-name()='project']/*[local-name()='name']/text()\" pom.xml",
				returnStdout: true
			).trim()
			if(name > "")
				Stage.parsedProjectName = name

			def version = script.sh(
				script: "xmllint --xpath \"/*[local-name()='project']/*[local-name()='version']/text()\" pom.xml",
				returnStdout: true
			).trim()
			if(version > "")
				Stage.parsedProjectVersion = version
		}
	}


	static def buildMaven(script) {
		parseProjectParameters(script, 'pom.xml')  // Inicializa {{ variables de remplazo }}
		script.sh 'mvn clean && mvn compile'
	}


	static def testJUnit(script) {
		script.sh '''set +x
			mvn \
			-Dmaven.compile.skip=true \
			test
		'''
		def exitCode = script.sh(
			script: 'find target/surefire-reports -name TEST*.xml | grep .',
			returnStatus: true)
		if(exitCode == 0) {
			script.junit 'target/surefire-reports/TEST*.xml'
		}
	}

}
