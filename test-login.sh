#!/bin/bash
cd /tmp
echo '{"username":"admin","password":"123456"}' > login.json
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d @login.json
