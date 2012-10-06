import logging

from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
from google.appengine.ext.webapp.util import run_wsgi_app
from language import pages


# loading the mappings
mappings = [
    ('/', pages.IndexPage),
    ('/sentences', pages.AllSentencesPage),
    ('/sentences/(.*)', pages.SingleSentencesPage),
    ('/list/history', pages.HistoryPage),
    ('/save/assessment', pages.SaveAssessment),
    ('/save/assessments', pages.SaveAssessmentGroup),
    ('/import', pages.TempPage)
    ]

#    ('/spel/(.*)', pages.GamePage),

# initialize the app with the mappings
application = webapp.WSGIApplication(mappings,debug=True)


def main():
    # Set the logging level in the main function
    # See the section on Requests and App Caching for information on how
    # App Engine reuses your request handlers when you specify a main function
    logging.getLogger().setLevel(logging.DEBUG)
    run_wsgi_app(application)


if __name__ == '__main__':
    main()





