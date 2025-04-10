#!/bin/bash
#
# IGinX - the polystore system with high performance
# Copyright (C) Tsinghua University
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

port=$1
pwd
dir

if netstat -an | grep -q ".*:$port.*LISTEN"; then
  echo "Port $port is open."
else
  echo "Port $port is not open."
fi

echo "stop neo4j $port"
powershell -command "Start-Process -FilePath 'neo4j_instances\\${port}\\bin\\neo4j.bat' -ArgumentList 'stop' -Wait"
#powershell -command "Start-Process -FilePath 'influxdb2-2.0.7-windows-amd64-$port/influxd' $arguments -NoNewWindow $redirect"
echo "stopped OK."
sleep 10

if netstat -an | grep -q ".*:$port.*LISTEN"; then
  echo "Port $port is open."
else
  echo "Port $port is not open."
fi

sleep 10

port1=8888
if netstat -an | grep -q ".*:$port1.*LISTEN"; then
  echo "Port $port1 is open."
else
  echo "Port $port1 is not open."
fi

if netstat -an | grep -q ".*:$port.*LISTEN"; then
  echo "Port $port is open."
else
  echo "Port $port is not open."
fi
#powershell -Command "Start-Process -FilePath ./neo4j_instances/$port/bin/neo4j.bat -ArgumentList stop -NoNewWindow -Wait"
