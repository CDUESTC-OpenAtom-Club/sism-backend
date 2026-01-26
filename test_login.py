#!/usr/bin/env python3
import requests
import json

url = 'http://localhost:8080/api/auth/login'
data = {'username': 'admin', 'password': '123456'}

try:
    r = requests.post(url, json=data)
    print(f'Status: {r.status_code}')
    print(f'Response: {r.text}')
except Exception as e:
    print(f'Error: {e}')
