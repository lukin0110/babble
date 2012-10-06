__author__ = 'maarten'

def force_unicode(string):
    if type(string) == unicode:
        return string
    return string.decode('utf-8')

def force_utf8(string):
    if type(string) == str:
        return string
    return string.encode('utf-8')
