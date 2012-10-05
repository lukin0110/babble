# -*- coding: utf-8 -*-
from models import Sentence

data =[

    {'en': 'Where is the library?',
     'nl': 'Waar is de bibliotheek?',
     'es': '¿Dónde está la biblioteca?',
     'fr': 'Où est la bibliothèque?',
     'ru': 'где библиотека?',
     },

    {'en': "What's your name",
     'nl': 'Hoe heet je?',
     'es': '¿Como te llamas?',
     'fr': "Comment tu t'appelles?",
     'ru': ' ',
     },
]


def importSentences(data):
    counter = 0
    for sent in data:
        for iso, langstring in sent:
            Sentence(value = force_unicode(langstring),group = str(counter),locale = iso).save()
        counter += 1

def force_unicode(string):
    if type(string) == unicode:
        return string
    return string.decode('utf-8')