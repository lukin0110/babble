# -*- coding: utf-8 -*-
import base64
import random
import string
import urllib
from django.template import TemplateDoesNotExist
from models import *
import json, importer, data, random, utils

import logging
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template

LOG = logging.getLogger("pages")

class IndexPage(webapp.RequestHandler):
    def get(self):
        values = {
            "var1":"hallo",
        }
        self.response.out.write(template.render('templates/index.html', values))

class TempPage(webapp.RequestHandler):
    def get(self):
        importer.importSentences(data.list)
        self.response.out.write("Done ... ")

class SentencesPage(webapp.RequestHandler):
    def get(self):
        results = {}
        list = Sentence.all()

        for sentence in list:
            group = results.get(sentence.group)

            if not group:
                group = {}
                results[sentence.group] = group

            group[sentence.locale] = {
                "id": sentence.key().id(),
                "value": sentence.value
            }

        results2 = []
        for key, value in results.items():
            results2.append({"group":key, "values": value})

        self.response.headers["Content-Type"] = "text/plain; charset=utf-8"
        self.response.out.write(json.dumps({"list": results2}, indent=4))

class HistoryPage(webapp.RequestHandler):
    def get(self):
        values = {}

class SaveAssessment(webapp.RequestHandler):
    def get(self): # its easier to test this functionality with a get method
        self.post()

    def post(self):
        values = {}
        self.response.headers["Content-Type"] = "text/plain; charset=utf-8"
        self.response.out.write(json.dumps({"test": "d"}, indent=4))

