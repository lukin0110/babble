import base64
import random
import string
import urllib
from django.template import TemplateDoesNotExist

import logging
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template


class IndexPage(webapp.RequestHandler):
    def get(self):
        values = {
            "var1":"hallo",

        }
        values["var1"]='test'
        self.response.out.write(template.render('templates/index.html', values))

