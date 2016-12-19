CREATE TABLE IF NOT EXISTS `message` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `to` varchar(36) NOT NULL,
  `from` varchar(36) NOT NULL,
  `body` text NOT NULL,
  `date_sent` bigint(20) NOT NULL,
  `read` bit(1) DEFAULT NULL,
  `notified` bit(1) DEFAULT NULL,
  `source_server` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32448 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `user` (
  `uuid` varchar(36) NOT NULL,
  `last_username` varchar(16) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `last_display_name` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;