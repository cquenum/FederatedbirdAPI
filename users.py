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

#####
# You can read users from a local file
# load json array from file
# uncomment the code below
# users = []
# with open("users.json") as json_file:
#    users = json.loads(json_file)
#####

# send post request for each user to add
for user in users:
    # set server url value
    url= 'http://localhost:8080/users'

    # specify the content-type in the header
    headers = { 'Content-Type' : 'application/json' }

    # POST user data
    r = requests.post(url, data=json.dumps(user),headers=headers)

    # Show error message that occurred
    print (r.text)
    print (r.status_code)

# TODO: Test cases