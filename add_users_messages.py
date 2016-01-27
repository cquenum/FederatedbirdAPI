#!/usr/bin/env python

# You must have install requests
# use the commands below to get it install
# sudo easy_install pip
# sudo pip install requests

import json
import requests

# get users from http://beta.json-generator.com/api/json/get/4JKuvC1Ug
# and parse to json
url = 'http://beta.json-generator.com/api/json/get/4JKuvC1Ug'
r = requests.get(url)
users = []
users = r.json()

# get messages from http://beta.json-generator.com/api/json/get/EJs_KTlUx
# and parse to json
url = 'http://beta.json-generator.com/api/json/get/VkznAGWLx'
r = requests.get(url)
messages = []
messages = r.json()


#####
# You can read users from a local file
# load json array from file
# uncomment the code below
# users = []
# with open("users.json") as json_file:
#    users = json.load(json_file)
#####


#####
# You can read messages from a local file
# load json array from file
# uncomment the code below
# messages = []
# with open("messages.json") as json_file:
#    messages = json.load(json_file)
#####

# send post request for each user to add
for user in users:
    # set server url value
    url= 'http://localhost:8080/users'

    # specify the content-type in the header
    headers = { 'Content-Type' : 'application/json' }

    # POST user data
    r = requests.post(url, data=json.dumps(user),headers=headers)

    # if the user is successfully added to the datatstore
    # then add messages
    if r.status_code == requests.codes.ok:
        # get the token from response
        token = str(r.text).encode("utf-8").replace("\"","")
        authorization = "Bearer " + token
        # set the header
        headers = { 'Content-Type' : 'application/json',
                    'Authorization': authorization }

        # Set url
        url = 'http://localhost:8080/messages'

        # Add messages
        for message in messages:
            r = requests.post(url, data=json.dumps(message),headers=headers)
            print (r.text)
            print (r.status_code)
    else:
        # Show error message that occurred
        print (r.text)
        print (r.status_code)

# TODO: Test cases