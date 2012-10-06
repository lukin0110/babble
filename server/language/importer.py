# -*- coding: utf-8 -*-
from models import Sentence
import utils

def importSentences(data):
    counter = 0
    for sent in data:
        for iso, langstring in sent.items():
            Sentence(value = utils.force_unicode(langstring),group = str(counter),locale = iso).save()
        counter += 1
