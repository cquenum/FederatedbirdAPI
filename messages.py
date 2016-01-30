#!/usr/bin/env python

# Created by cquenum on 17/12/2015.

# You must have install requests
# use the commands below to get it install
# sudo easy_install pip
# sudo pip install requests

import json
import random
import requests

# get messages from http://beta.json-generator.com/api/json/get/EJs_KTlUx
# and parse to json
# url = 'http://beta.json-generator.com/api/json/get/EJs_KTlUx'
url = 'http://beta.json-generator.com/api/json/get/VkznAGWLx'
r = requests.get(url)
messages = []
messages = r.json()


#####
# You can read messages from a local file
# load json array from file
# uncomment the code below
# messages = []
# with open("messages.json") as json_file:
#    messages = json.load(json_file)
#####

# User token is required to post a message
# Get all the users
# set server url value
url= 'http://beta.json-generator.com/api/json/get/4JKuvC1Ug'
r = requests.get(url)
# users = []
users = r.json()

# send post request for each message
# then display response and status code
for message in messages:
    # Pick a random user and get his token
    user = random.choice(users)
    headers = { 'Content-Type' : 'application/json'}
    url = 'http://localhost:8080/auth/token'
    r = requests.post(url, data=json.dumps(user),headers=headers)

    if r.status_code == requests.codes.ok:
        token = str(r.text).encode("utf-8").replace("\"","")
        authorization = "Bearer " + token
        headers = { 'Content-Type' : 'application/json',
                    'Authorization': authorization,
                    'followed': 'true'}
        url = 'http://localhost:8080/messages'
        r = requests.post(url, data=json.dumps(message),headers=headers)
        print (r.text)
        print (r.status_code)
        id = random.randint(1, 100)
        r = requests.post('http://localhost:8080/users/' + str(id), data=json.dumps(message), headers=headers)
        print (r.text)
        print (r.status_code)
# TODO: Test cases