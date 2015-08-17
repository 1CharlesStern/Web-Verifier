DIR=${PWD##*/} 
if DIR="logFiles"; 
then
find $( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd ) -mtime +1 -exec rm {} \;
else
echo "Move this file to the logFiles directory, it is not meant to be used outside of it!"
fi