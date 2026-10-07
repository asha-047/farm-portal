$image = 'YOUR_DOCKERHUB_USER/farm-portal'
$tag   = $facts['farm_tag'] ? { undef => 'latest', default => $facts['farm_tag'] }

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

# Runs the container only if the right version is not already running
exec { 'run-farm-container':
  provider => shell,
  command  => "docker rm -f farm; docker run -d --name farm --restart always -p 8083:8081 ${image}:${tag}",
  unless   => "docker ps --filter name=farm --filter ancestor=${image}:${tag} -q | grep -q .",
  path     => ['/bin', '/usr/bin'],
  require  => Service['docker'],
}
