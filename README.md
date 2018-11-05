# Docker Build Step Plugin

This plugin allows adding various Docker commands into your Jenkins Freestyle job as a build step.

# Setup

## Set Docker URL

Jenkins -> Manage Jenkins -> Configure System -> Docker Builder

Docker URL Field
* Configures Docker server REST API URL
* For Linux hosts, set the local socket `unix:///var/run/docker.sock`
* Test the connection.

# Usage

## Commands

In the build area of ​​Jenkins Job, add the build step

    Execute Docker command

There is a range of Docker Command options, roughly correlating with the Docker CLI commands.

![Docker Commands](images/dbc_docker_commands_menu.png "Docker Commands")

## Docker Pull

### Fields

| Field | Description |
| ----- | ----- |
| Name of the image to pull (repository/image:tag) |  base name of the image |
| Tag | Actual tag, e.g. `3.4.5` or `latest` |
| Registry | hostname of registry used to store images e.g. `mydocker.mycompany.com` |
| Docker registry URL | registry URL to use for pull authentication, e.g. `https://mydocker.mycompany.com` |
| Registry Credentials | ...Choose correct credential here from the list... |

![Docker Pull Command](images/dbc_pull_image.png "Docker Pull Command")

## Docker Tag

### Fields

| Field | Description |
| ----- | ----- |
| Name of the image to tag (repository /image:tag) |  base name of the image |
| Target repository of the new tag | destination repository/image name e.g. `dac/nodeynode` |
| The tag to set | Actual destination tag, e.g. `3.4.5` or `latest` |

![Docker Tag Command](images/dbc_tag_image.png "Docker Tag Command")

### Scenario 1: Tag a Dockerhub image with a local tag

#### Effective Docker command line

    docker tag library/node:8.9.4 dac/nodeynode:8.9.4

#### Settings

| Field | Value |
| ----- | ----- |
| Name of the image to tag (repository /image:tag) | library/node:8.9.4 |
| Target repository of the new tag | dac/nodeynode |
| The tag to set | 8.9.4 |

#### Result

    [Docker] INFO: Done
    [Docker] INFO: start tagging image library/node:8.9.4 in dac/nodeynode as 8.9.4
    [Docker] INFO: Tagged image library/node:8.9.4 in dac/nodeynode as 8.9.4

### Scenario 2: Tag a private repo image with a local tag

#### Effective Docker command line

    docker tag mydocker.mycompany.com/library/node:4.4.6 dac/nodeynode:4.4.6

#### Settings

| Field | Value |
| ----- | ----- |
| Name of the image to tag (repository/image:tag) | mydocker.mycompany.com/library/node:4.4.6 |
| Target repository of the new tag | dac/nodeynode |
| The tag to set | 4.4.6 |

#### Result

    [Docker] INFO: Done
    [Docker] INFO: start tagging image mydocker.mycompany.com/library/node:4.4.6 in dac/nodeynode as 4.4.6
    [Docker] INFO: Tagged image mydocker.mycompany.com/library/node:4.4.6 in dac/nodeynode as 4.4.6

## Docker Push

### Fields

| Field | Description |
| ----- | ----- |
| Name of the image to push (repository /image) | base name of the image |
| Tag | Actual tag, e.g. `3.4.5` or `latest` |
| Registry | hostname of registry used to store images e.g. `mydocker.mycompany.com` |
| Docker registry | registry hostname to use for authentication, e.g. `mydocker.mycompany.com` |
| Registry Credentials | ...Choose correct credential here from the list... |

![Docker Push Command](images/dbc_push_image.png "Docker Push Command")

### Scenario 1: Push a local image to a private repo

#### Effective Docker command line

    docker login ...
    docker push mydocker.mycompany.com/dac/nodeynode:4.4.6  

#### Settings

| Field | Value |
| ----- | ----- |
| Name of the image to push (repository /image) | dac/nodeynode |
| Tag| 4.4.6 |
| Registry | mydocker.mycompany.com |
| Docker registry | mydocker.mycompany.com |
| Registry Credentials | ...Choose correct credential here from the list... |

#### Result

    [Docker] INFO: Pushing image mydocker.mycompany.com/dac/nodeynode:4.4.6
    [Docker] INFO: PushResponseItem[stream=<null>,status=The push refers to repository [mydocker.mycompany.com/dac/nodeynode],progressDetail=<null>,progress=<null>,id=<null>,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    [Docker] INFO: PushResponseItem[stream=<null>,status=Preparing,progressDetail=ResponseItem.ProgressDetail[current=<null>,total=<null>,start=<null>],progress=<null>,id=c747e356ef2e,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    …
    [==================================================>]  326.3MB,id=ec0200a19d76,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    [Docker] INFO: PushResponseItem[stream=<null>,status=Pushing,progressDetail=ResponseItem.ProgressDetail[current=326362624,total=318467546,start=<null>],progress=[==================================================>]  326.4MB,id=ec0200a19d76,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    [Docker] INFO: PushResponseItem[stream=<null>,status=Pushed,progressDetail=ResponseItem.ProgressDetail[current=<null>,total=<null>,start=<null>],progress=<null>,id=ec0200a19d76,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    [Docker] INFO: PushResponseItem[stream=<null>,status=4.4.6: digest: sha256:bbdd44f8824f93e3da152db5b425fd28bcab03f6746efa7ef1e20555ff21e8bd size: 1587,progressDetail=<null>,progress=<null>,id=<null>,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=<null>]
    [Docker] INFO: PushResponseItem[stream=<null>,status=<null>,progressDetail=ResponseItem.ProgressDetail[current=<null>,total=<null>,start=<null>],progress=<null>,id=<null>,from=<null>,time=<null>,errorDetail=<null>,error=<null>,aux=ResponseItem.AuxDetail[size=1587,tag=4.4.6,digest=sha256:bbdd44f8824f93e3da152db5b425fd28bcab03f6746efa7ef1e20555ff21e8bd]]
    [Docker] INFO: Done pushing image mydocker.mycompany.com/dac/nodeynode:4.4.6


<!-- ## Docker Build and Publish -->

# Credits

Uses components from [Docker Commons Plugin](https://wiki.jenkins.io/display/JENKINS/Docker+Commons+Plugin) which provides APIs for other Docker-related plugins

Some documentation and materials adapted from [Website, Chinese, NSFW?](http://www.bkjia.com/Linux/1037945.html)

# License

See [License](./LICENSE)