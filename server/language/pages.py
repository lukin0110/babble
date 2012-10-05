import base64
import random
import string
import urllib
from django.template import TemplateDoesNotExist
import json
from models import *
import importer

import logging
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template


class IndexPage(webapp.RequestHandler):
    def get(self):
        values = {}
        self.response.out.write(template.render('templates/index.html', values))

class SentencesPage(webapp.RequestHandler):
    def get(self):
        values = {}
        self.response.out.write(json.dumps({"test": "gelukt"}))

class HistoryPage(webapp.RequestHandler):
    def get(self):
        values = {}

class SaveAssessment(webapp.RequestHandler):
    def post(self):
        values = {}


class TempPage(webapp.RequestHandler):
    def get(self):
        importer.importSentences(importer.data)
        #for sent in importer.data:
        #    self.response.out.write(sent["nl"])

        self.response.out.write("Done ... ")
