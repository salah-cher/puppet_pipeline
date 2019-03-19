@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.20')
])
import hudson.FilePath
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor

// Class to load the YAML
class ProjectConfig {
    String project
    String downstream_job
    String downstream_job1
    String repo = "https://github.tools.sap/C5280113/puppet_mgmt_automation.git"
    String repo_credentials = "jenkinsAccessBlueOcean"
    String branch = "master"
    String playbook
    String ansible_ssh_key = "ansible_ssh_key"
    String ansible_vault_passwd = "ansible_vault_passwd"
    String server_type
    String ping_test
    String ansible_inventory = "Generated_Ansible_Inventory"
    short servernum = 2
    String service_user
    ArrayList parameters = []
    Map<String, String> service_tools = [:]
}

// JobDSL Template
class DeployTemplate {
    static boolean isSecret(name) {
        return name.matches(".*(?i:key|pass|secret).*")
    }
	

    // create deployment job except rundeck
    static void create(job, config) {
        job.with {
            description("Deploy ${config.project}. This job is managed via JobDSL; any manual changes will be lost.")

            wrappers {
                preBuildCleanup()
                colorizeOutput()
            }

            logRotator {
                artifactDaysToKeep(7)
                daysToKeep(90)
            }

            parameters {
                stringParam('GIT_BRANCH', config.branch, 'Git Branch to be used for Ansible repo')
            }

            scm {
                git {
                    remote {
                        url(config.repo)
                        credentials(config.repo_credentials)
                    }
                    branch(config.branch)
                }
            }

            steps {
                ansiblePlaybook(config.playbook) {
                inventoryPath(config.ansible_inventory)
                disableHostKeyChecking(true)  
		credentialsId(config.ansible_ssh_key)
                vaultCredentialsId(config.ansible_vault_passwd)
                colorizedOutput(true)
                forks(20)
                }
		   // shell(readFileFromWorkspace('uphosts.sh'))

            }
		if (config.ping_test)
			   {
			   steps {
			shell(readFileFromWorkspace('uphosts.sh'))
			         }
			  }
			if (config.downstream_job)
			{
				publishers {
                     downstream(config.downstream_job, 'SUCCESS')
					}
					
			}       

           if (config.downstream_job1)
			{
			publishers {
                           downstreamParameterized {
                               trigger(config.downstream_job1) {
                               condition('FAILED')
                                   parameters {
                                      currentBuild()
                                             }
                                           }
                                        }
                        }
			}
		}
    }
}
def getEnvironment() {
    def hostName = InetAddress.localHost.getHostName()
    String[] str
    splitHostName = hostName.split('-')
    def dataCenter = splitHostName[0]
    def env = splitHostName[2][-1..-1]
    switch (env) {
        case "p":
            env = "prod"
            break
        case "s":
            env = "stag"
            break
        case "d":
            env = "dev"
            break
        case "q":
            env = "qa"
            break
        default:
            env = "SOMETHING_WENT_WRONG"
            break
    }
    return [dataCenter, env]
}

def getConfigFiles(dir) {
    if (!dir.isDirectory()) {
        return [:]
    }
    def fileList = dir.list('*.yml')
    def fileMap = fileList.collectEntries {
        [it.name , it]
    }
    return fileMap
}

void createJobs(String dataCenter, String env) 


{
    def constr = new CustomClassLoaderConstructor(this.class.classLoader)
    def yaml = new Yaml(constr)

    // List all *.yml files
    def cwd = hudson.model.Executor.currentExecutor().currentWorkspace.absolutize()
    def configsGlobal = getConfigFiles(new FilePath(cwd, 'configs/'))
    def configsDC     = getConfigFiles(new FilePath(cwd, 'configs/' + dataCenter))
    def configsDCEnv  = getConfigFiles(new FilePath(cwd, 'configs/' + dataCenter + '/' + env))
    def configFiles = configsGlobal + configsDC + configsDCEnv
    println configFiles.values().toString().replace(',', '\n')

    def nonDisruptive = false

    // Create/update a pull request job for each config file
    configFiles.values().each { file ->
        def projectConfig = yaml.loadAs(file.readToString(), ProjectConfig.class)

        // create jenkins job
    DeployTemplate.create(job(projectConfig.project), projectConfig)
        
    }
}

// MAIN
def (dataCenter, env) = getEnvironment()

createJobs(dataCenter, env)
