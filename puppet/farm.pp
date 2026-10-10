$image     = 'asha047/farm-portal'
$tag       = $facts['farm_tag'] ? { undef => 'latest', default => $facts['farm_tag'] }
$container = 'farm-puppet'
$host_port = '8084'

package { 'docker.io':
  ensure => installed,
}

service { 'docker':
  ensure  => running,
  enable  => true,
  require => Package['docker.io'],
}

group { 'farm':
  ensure => present,
}

user { 'farm':
  ensure  => present,
  gid     => 'farm',
  home    => '/opt/farm',
  shell   => '/bin/bash',
  require => Group['farm'],
}

file { '/opt/farm':
  ensure  => directory,
  owner   => 'farm',
  group   => 'farm',
  mode    => '0755',
  require => User['farm'],
}

file { '/opt/farm/deployed-version.txt':
  ensure  => file,
  content => "${image}:${tag}\n",
  owner   => 'farm',
  group   => 'farm',
  require => File['/opt/farm'],
}

# Starts the container only if the wanted version is not already running
exec { 'run-farm-container':
  provider => shell,
  command  => "docker rm -f ${container}; docker run -d --name ${container} --restart always -p ${host_port}:8081 ${image}:${tag}",
  unless   => "docker ps --filter name=${container} --filter ancestor=${image}:${tag} -q | grep -q .",
  path     => ['/bin', '/usr/bin'],
  timeout  => 600,
  require  => [Service['docker'], File['/opt/farm/deployed-version.txt']],
}